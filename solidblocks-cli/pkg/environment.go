package pkg

import (
	"fmt"
	"os"
	"reflect"
)

type ValueFrom struct {
	Task string
}

type Transform struct {
	Json string
}

type EnvironmentVariable struct {
	Name      string
	Value     string
	ValueFrom *ValueFrom
	Transform *Transform
}

func (variable EnvironmentVariable) LogPlanText() string {

	if len(variable.Value) > 0 {
		return fmt.Sprintf("variable '%s' will be set to '%s'", TextBoldBlack(variable.Name), TextBoldBlack(variable.Value))
	} else {
		return fmt.Sprintf("value for variable '%s' will inherited from the calling environment", TextBoldBlack(variable.Name))
	}
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
				Outputf("environment variable '%s' defined as inherited from parent but is not set", environment.Name)
			}
			result[environment.Name] = parent
		}
	}

	return result
}

func ParseEnvironment(data interface{}) ([]EnvironmentVariable, error) {

	enVars := make([]EnvironmentVariable, 0)

	environment, err := GetSliceKey("environment", data)
	if environment != nil && err == nil {
		for _, environmentRaw := range environment {

			name, err := GetStringKey("name", environmentRaw)
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

			valueFromObject := GetOptionalObjectKey("valueFrom", environmentRaw)
			if valueFromObject != nil && valueFromObject["task"] != nil {
				taskValueOf := reflect.ValueOf(valueFromObject["task"])
				if taskValueOf.Kind() == reflect.String {
					enVars = append(enVars, EnvironmentVariable{Name: name, Transform: transform, ValueFrom: &ValueFrom{Task: taskValueOf.String()}})
					continue
				}
			}

			value := GetOptionalStringKey("value", environmentRaw)

			if len(name) > 0 {
				enVars = append(enVars, EnvironmentVariable{Name: name, Transform: transform, Value: value})
				continue
			}
		}
	}

	return enVars, nil
}
