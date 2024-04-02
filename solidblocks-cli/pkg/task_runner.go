package pkg

import (
	"errors"
	"fmt"
	"github.com/urfave/cli/v2"
)

var taskRunners = make([]TaskRunnerRegistration, 0)

type TaskRunnerRegistration interface {
	Id() string
	Parse(taskRaw map[string]interface{}) (TaskRunner, error)
	Help() string
}

func RegisterTaskRunner(taskRunner TaskRunnerRegistration) {
	taskRunners = append(taskRunners, taskRunner)
}

type TaskRunner interface {
	Run(environment map[string]string) *TaskRunnerResult
}

type TaskRunnerResult struct {
	Success bool
	Output  string
}

func parseTaskRunner(data map[string]interface{}) (TaskRunner, error) {
	key, data, err := GetKeyAndData(data)
	if err != nil {
		return nil, err
	}

	for _, taskRunner := range taskRunners {
		if key == taskRunner.Id() {
			runner, err := taskRunner.Parse(data)
			if err != nil {
				return nil, err
			}

			return runner, nil
		}
	}

	return nil, errors.New(fmt.Sprintf("runner '%s' not found", key))
}

func HasTaskRunner(runnerId string) bool {
	return GetTaskRunner(runnerId) != nil
}

func GetTaskRunner(runnerId string) TaskRunnerRegistration {
	for _, taskRunner := range taskRunners {
		if runnerId == taskRunner.Id() {
			return taskRunner
		}
	}

	return nil
}

var WorkflowListTaskRunnersCommand = cli.Command{
	Name:  "runners",
	Usage: "show a list of all available task runners",
	Action: func(context *cli.Context) error {

		if context.Args().Len() == 1 && HasTaskRunner(context.Args().Get(0)) {
			println(GetTaskRunner(context.Args().Get(0)).Help())
			return cli.Exit("", 0)
		}

		println("the following runners are available")
		println("")
		for _, taskRunner := range taskRunners {
			println(fmt.Sprintf("  * %s", taskRunner.Id()))
		}
		println("")
		println("to show help for a runner run")
		println("")
		println("blcks workflow runners <runner>")

		return cli.Exit("", 0)
	},
}
