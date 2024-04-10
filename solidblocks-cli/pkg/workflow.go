package pkg

import (
	"embed"
	"errors"
	"fmt"
	"github.com/goccy/go-yaml"
	"github.com/urfave/cli/v2"
	"os"
)

type Task struct {
	Name        string
	Environment []EnvironmentVariable
	Runner      TaskRunner
}

type Workflow struct {
	Name        string
	Tasks       []Task
	Environment []EnvironmentVariable
}

func WorkflowParse(name string, data []byte) (*Workflow, error) {

	workflowRaw := make(map[string]interface{})
	workflow := Workflow{Name: name}

	err := yaml.Unmarshal(data, &workflowRaw)
	if err != nil {
		return nil, err
	}

	tasks, err := GetSliceKey("tasks", workflowRaw)
	if tasks != nil && err == nil {
		err := parseTasks(tasks, &workflow)

		if err != nil {
			return nil, err
		}
	}

	envVars, err := ParseEnvironment(workflowRaw)
	if err != nil {
		return nil, err
	}
	workflow.Environment = envVars

	return &workflow, nil
}

func WorkflowValidate(workflow *Workflow) []error {

	validationErrors := make([]error, 0)

	for _, envVarName := range workflow.GetEnvVarNames("") {
		if workflow.CountEnvVars(envVarName) > 1 {
			validationErrors = append(validationErrors, errors.New(fmt.Sprintf("environment variable '%s' defined more than once", envVarName)))
		}
	}

	for _, task := range workflow.Tasks {
		for _, envVar := range task.Environment {
			if envVar.ValueFrom != nil {
				if len(envVar.ValueFrom.Task) > 0 {
					if !workflow.HasTask(envVar.ValueFrom.Task) {
						validationErrors = append(validationErrors, errors.New(fmt.Sprintf("environment variable '%s' references task '%s' which does not exist", envVar.Name, envVar.ValueFrom.Task)))
					}
				}
			}
		}
	}

	return validationErrors
}

func (workflow *Workflow) HasTask(taskName string) bool {
	return workflow.getTask(taskName) != nil
}

func (workflow *Workflow) getTask(taskName string) *Task {
	for _, task := range workflow.Tasks {
		if task.Name == taskName {
			return &task
		}
	}

	return nil
}

func parseTasks(data []interface{}, workflow *Workflow) error {

	for _, taskRaw := range data {
		key, object, err := GetKeyAndData(taskRaw)
		if err != nil {
			return errors.New(fmt.Sprintf("%s: %s", key, err.Error()))
		}

		task, err := ParseTask(key, object)
		if err != nil {
			return errors.New(fmt.Sprintf("%s: %s", key, err.Error()))
		}

		workflow.Tasks = append(workflow.Tasks, *task)
	}

	return nil
}

func ParseTask(key string, data map[string]interface{}) (*Task, error) {
	runner, err := ParseTaskRunner(data)
	if err != nil {
		return nil, err
	}

	envVars := make([]EnvironmentVariable, 0)

	envVars, err = ParseEnvironment(data)
	if err != nil {
		return nil, err
	}

	return &Task{key, envVars, runner}, nil
}

func ParseWorkflows(context *cli.Context) ([]*Workflow, error) {

	workflows := make([]*Workflow, 0)

	if context.Args().Len() == 0 {
		return nil, errors.New("no workflow provided")
	}

	for _, arg := range context.Args().Slice() {
		_, err := os.Stat(arg)

		if err != nil {
			if errors.Is(err, os.ErrNotExist) {
				return nil, errors.New(fmt.Sprintf("workflow '%s' not found", arg))
			}

			return nil, err
		}

		Outputf("loading workflow '%s'", arg)
		workflowData, err := os.ReadFile(arg)
		if err != nil {
			return nil, err
		}

		Outputf("parsing workflow '%s'", arg)
		workflow, err := WorkflowParse(arg, workflowData)
		if err != nil {
			return nil, errors.New(fmt.Sprintf("failed to parse workflow '%s', %s", arg, err.Error()))
		}

		workflows = append(workflows, workflow)
	}

	return workflows, nil
}

//go:embed workflow_help.md
var workflowHelp embed.FS

var WorkflowCommand = cli.Command{
	Name:        "workflow",
	Usage:       "execute tasks using the Solidblocks workflow manifest  (experimental)",
	Description: RenderHelp(workflowHelp, "workflow_help.md"),
	Action: func(cCtx *cli.Context) error {
		return cli.Exit("", 0)
	},
	Subcommands: []*cli.Command{
		&WorkflowPlanCommand,
		&WorkflowRunCommand,
		&WorkflowListTaskRunnersCommand,
	},
}
