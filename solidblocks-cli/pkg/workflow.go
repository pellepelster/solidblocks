package pkg

import (
	"embed"
	"errors"
	"fmt"
	"github.com/goccy/go-yaml"
	"github.com/urfave/cli/v2"
	"os"
)

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

var ParseWorkflowsArgHelp = "[workflow manifest(s)] (defaults to workflow.yml)"

func ParseWorkflows(context *cli.Context) ([]*Workflow, error) {

	workflowFiles := make([]string, 0)

	if context.Args().Len() == 0 {
		file, _ := os.Stat("workflow.yml")
		if file != nil {
			workflowFiles = append(workflowFiles, file.Name())
		} else {
			return nil, errors.New("no workflow provided")
		}
	} else {
		for _, arg := range context.Args().Slice() {
			workflowFiles = append(workflowFiles, arg)
		}
	}

	workflows := make([]*Workflow, 0)
	for _, workflowFile := range workflowFiles {
		_, err := os.Stat(workflowFile)

		if err != nil {
			if errors.Is(err, os.ErrNotExist) {
				return nil, errors.New(fmt.Sprintf("workflow '%s' not found", workflowFile))
			}

			return nil, err
		}

		OutputDebugf(context, "loading workflow '%s'", workflowFile)
		workflowData, err := os.ReadFile(workflowFile)
		if err != nil {
			return nil, err
		}

		OutputDebugf(context, "parsing workflow '%s'", workflowFile)
		workflow, err := WorkflowParse(workflowFile, workflowData)
		if err != nil {
			return nil, errors.New(fmt.Sprintf("failed to parse workflow '%s', %s", workflowFile, err.Error()))
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
	Subcommands: []*cli.Command{
		&WorkflowPlanCommand,
		&WorkflowRunCommand,
		&WorkflowListTasks,
		&WorkflowListTaskRunnersCommand,
	},
	Action: ListTasksAction,
}
