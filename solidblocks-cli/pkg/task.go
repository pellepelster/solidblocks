package pkg

import (
	"fmt"
	"github.com/urfave/cli/v2"
	"reflect"
)

type Task struct {
	Name        string
	Description string
	Environment []EnvironmentVariable
	Runner      TaskRunner
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

	description := ""
	if data["description"] != nil {
		descriptionValue := reflect.ValueOf(data["description"])
		if descriptionValue.Kind() == reflect.String {
			description = descriptionValue.String()
		}
	}

	return &Task{key, description, envVars, runner}, nil
}

func (task Task) LogPlanText() string {
	return fmt.Sprintf("task %s will %s", TextPrimary(task.Name), task.Runner.LogPlanText())
}

var ListTasksAction = func(context *cli.Context) error {
	workflows, err := ParseWorkflows(context)
	if err != nil {
		return cli.Exit(err.Error(), 1)
	}

	Outputf(TextBoldBlack("workflow tasks"))
	Outputf(TextBoldBlack("--------------"))
	for _, workflow := range workflows {
		for _, task := range workflow.Tasks {
			if len(task.Description) > 0 {
				Outputf("%s - %s", TextPrimary(task.Name), TextSecondary(task.Description))
			} else {
				Outputf(TextPrimary(task.Name))
			}
		}
	}

	return cli.Exit("", 0)
}

var WorkflowListTasks = cli.Command{
	Name:      "tasks",
	Usage:     "show a list of all available tasks",
	ArgsUsage: ParseWorkflowsArgHelp,
	Action:    ListTasksAction,
}
