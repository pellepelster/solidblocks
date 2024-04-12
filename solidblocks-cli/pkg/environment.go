package pkg

import (
	"bytes"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"reflect"
	"strings"
)

type ValueFrom struct {
	Task    string
	Command []string
	Script  []string
}

type Transform struct {
	Json string
}

type EnvironmentVariable struct {
	Name        string
	Description string
	Value       string
	ValueFrom   *ValueFrom
	Transform   *Transform
	Default     *EnvironmentVariableDefault
}

type EnvironmentVariableDefault struct {
	Value     string
	ValueFrom *ValueFrom
}

func (workflow *Workflow) HasEnvVar(varName string) bool {
	return workflow.GetEnvVar(varName) != nil
}

func (workflow *Workflow) GetEnvVar(varName string) *EnvironmentVariable {
	envVars := workflow.GetEnvVars(varName)

	if len(envVars) == 1 {
		return &envVars[0]
	}

	return nil
}

func (workflow *Workflow) GetEnvVars(varName string) []EnvironmentVariable {
	envVars := make([]EnvironmentVariable, 0)

	for _, envVar := range workflow.Environment {
		if len(varName) == 0 || varName == envVar.Name {
			envVars = append(envVars, envVar)
		}
	}

	return envVars
}

func (workflow *Workflow) GetEnvVarNames(varName string) []string {
	varNames := make([]string, 0)

	for _, envVar := range workflow.GetEnvVars(varName) {
		varNames = append(varNames, envVar.Name)
	}

	return Unique(varNames)
}

func (workflow *Workflow) CountEnvVars(varName string) int {
	return len(workflow.GetEnvVars(varName))
}

func (workflow *Workflow) GetEnvVarsForTask(task *Task) map[string]string {

	result := make(map[string]string)
	for _, environment := range workflow.Environment {

		if len(environment.Value) > 0 {
			result[environment.Name] = environment.Value
		} else {
			parent := os.Getenv(environment.Name)

			if len(parent) == 0 {
				Outputfln("environment variable '%s' defined as inherited from parent but is not set", environment.Name)
			}
			result[environment.Name] = parent
		}
	}

	return result
}

func parseValueFrom(data interface{}) (*ValueFrom, error) {
	valueFromObject := GetOptionalObjectKey("valueFrom", data)
	if valueFromObject != nil {
		task := GetOptionalStringByKey("task", valueFromObject)

		if len(task) > 0 {
			return &ValueFrom{Task: task}, nil
		}

		script, err := GetAsStringList("script", valueFromObject)
		if err == nil {
			return &ValueFrom{Script: script}, nil
		}

		command, err := GetAsStringList("command", valueFromObject)
		if err == nil {
			return &ValueFrom{Command: command}, nil
		}
	}

	return nil, nil
}

func resolveCommand(command []string) (string, error) {
	var cmd *exec.Cmd
	cmd = exec.Command(command[0], command[1:]...)

	stdout := bytes.NewBufferString("")
	stderr := bytes.NewBufferString("")

	cmd.Stdout = stdout
	cmd.Stderr = stderr

	err := cmd.Start()
	if err != nil {
		return "", err
	}

	err = cmd.Wait()
	if err != nil {
		return "", err
	}

	return strings.TrimSpace(stdout.String()), nil
}

func resolveValueFrom(variableName string, valueFrom *ValueFrom) (string, error) {
	if len(valueFrom.Command) > 0 {
		value, err := resolveCommand(valueFrom.Command)
		if err != nil {
			return "", errors.New(fmt.Sprintf("failed to resolve variable '%s': %s", variableName, err.Error()))
		}

		return value, nil
	}

	if len(valueFrom.Script) > 0 {
		value, err := resolveCommand(append([]string{"/bin/sh"}, valueFrom.Script...))
		if err != nil {
			return "", errors.New(fmt.Sprintf("failed to resolve variable '%s': %s", variableName, err.Error()))
		}

		return value, nil
	}

	return "", nil
}

func EnvironmentResolve(variable EnvironmentVariable) (string, error) {
	if len(variable.Value) > 0 {
		return variable.Value, nil
	}

	value := os.Getenv(variable.Name)

	if len(value) > 0 {
		return value, nil
	}

	if variable.ValueFrom != nil {
		return resolveValueFrom(variable.Name, variable.ValueFrom)
	}

	if variable.Default != nil {
		if len(variable.Default.Value) > 0 {
			return variable.Default.Value, nil
		}

		if variable.Default.ValueFrom != nil {
			return resolveValueFrom(variable.Name, variable.Default.ValueFrom)
		}
	}

	return "", nil
}

func EnvironmentResolveHelp(variable EnvironmentVariable) string {
	if len(variable.Value) > 0 {
		return "static value"
	}

	if variable.Default != nil {
		if variable.Default.ValueFrom != nil {
			if len(variable.Default.ValueFrom.Command) > 0 {
				return fmt.Sprintf("output of command '%s' if not set", strings.Join(variable.Default.ValueFrom.Command, " "))
			}
			if len(variable.Default.ValueFrom.Script) > 0 {
				return fmt.Sprintf("output of script '%s' if not set", strings.Join(variable.Default.ValueFrom.Script, " "))
			}
		}
	}

	if variable.ValueFrom != nil {
		if len(variable.ValueFrom.Command) > 0 {
			return fmt.Sprintf("output of command '%s'", strings.Join(variable.ValueFrom.Command, " "))
		}
		if len(variable.ValueFrom.Script) > 0 {
			return fmt.Sprintf("output of script '%s'", strings.Join(variable.ValueFrom.Command, " "))
		}
	}

	return "<unknown>"
}

func EnvironmentParse(data interface{}) ([]EnvironmentVariable, error) {

	enVars := make([]EnvironmentVariable, 0)

	environment, err := GetSliceByKey("environment", data)
	if environment != nil && err == nil {
		for _, environmentRaw := range environment {

			name, err := GetStringByKey("name", environmentRaw)
			if err != nil {
				return nil, err
			}
			description := GetOptionalStringByKey("description", environmentRaw)

			def, err := parseDefault(environmentRaw)
			if err != nil {
				return nil, err
			}

			var transform *Transform = nil
			transformObject := GetOptionalObjectKey("transform", environmentRaw)
			if transformObject != nil && transformObject["json"] != nil {
				transformJson := reflect.ValueOf(transformObject["json"])
				if transformJson.Kind() == reflect.String {
					transform = &Transform{Json: transformJson.String()}
				}
			}

			valueFrom, err := parseValueFrom(environmentRaw)
			if err != nil {
				return nil, err
			}
			value := GetOptionalStringByKey("value", environmentRaw)

			if len(value) > 0 && valueFrom != nil {
				return nil, errors.New(fmt.Sprintf("variable %s: 'value' and 'valueFrom' must not be set at the same time", name))
			}

			if valueFrom != nil {
				enVars = append(enVars, EnvironmentVariable{Name: name, Description: description, Default: def, Transform: transform, ValueFrom: valueFrom})
				continue
			}

			if len(name) > 0 {
				enVars = append(enVars, EnvironmentVariable{Name: name, Description: description, Default: def, Transform: transform, Value: value})
				continue
			}
		}
	}

	return enVars, nil
}

func parseDefault(environmentRaw interface{}) (*EnvironmentVariableDefault, error) {
	defaultString := GetOptionalStringByKey("default", environmentRaw)
	if len(defaultString) > 0 {
		return &EnvironmentVariableDefault{Value: defaultString}, nil
	}

	defaultData := GetOptionalObjectKey("default", environmentRaw)
	if defaultData != nil {
		valueFrom, err := parseValueFrom(defaultData)
		if err != nil {
			return nil, err
		}

		if valueFrom != nil {
			return &EnvironmentVariableDefault{ValueFrom: valueFrom}, nil
		}

		value := GetOptionalStringByKey("value", defaultData)
		if len(value) > 0 {
			return &EnvironmentVariableDefault{Value: value}, nil
		}

		if len(value) > 0 {
			return &EnvironmentVariableDefault{Value: value}, nil
		}
	}

	return nil, nil
}
