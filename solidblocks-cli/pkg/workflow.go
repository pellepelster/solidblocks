package pkg

import (
	"embed"
	"errors"
	"fmt"
	"github.com/charmbracelet/log"
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

func ParseWorkflow(name string, data []byte) (*Workflow, error) {

	workflowRaw := make(map[string]interface{})
	workflow := Workflow{Name: name}

	err := yaml.Unmarshal(data, &workflowRaw)

	tasks, err := GetSliceKey("tasks", workflowRaw)
	if tasks != nil && err == nil {
		err := parseTasks(tasks, &workflow)
		if err != nil {
			return nil, err
		}
	}

	environment, err := GetSliceKey("environment", workflowRaw)
	if environment != nil && err == nil {
		err := parseEnvironment(environment, &workflow)
		if err != nil {
			return nil, err
		}
	}

	return &workflow, nil
}

func (workflow *Workflow) hasTask(taskName string) bool {
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
			return err
		}

		task, err := parseTask(key, object)
		if err != nil {
			return err
		}

		workflow.Tasks = append(workflow.Tasks, *task)
	}

	return nil
}

func parseEnvironment(data []interface{}, workflow *Workflow) error {

	for _, environmentRaw := range data {

		name, err := GetStringKey("name", environmentRaw)
		if err != nil {
			return err
		}

		value, _ := GetStringKey("value", environmentRaw)

		if len(name) > 0 {
			workflow.Environment = append(workflow.Environment, EnvironmentVariable{Name: name, Value: value})
		}
	}

	return nil
}

func parseTask(key string, data map[string]interface{}) (*Task, error) {
	runner, err := parseTaskRunner(data)
	if err != nil {
		return nil, errors.New(fmt.Sprintf("%s: %s", key, err))
	}

	return &Task{key, []EnvironmentVariable{}, runner}, nil
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

		log.Infof("loading workflow '%s'", arg)
		workflowData, err := os.ReadFile(arg)
		if err != nil {
			return nil, err
		}

		log.Infof("parsing workflow '%s'", arg)
		workflow, err := ParseWorkflow(arg, workflowData)
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
