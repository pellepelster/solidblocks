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

type TaskOutputLogger func(string)

type TaskRunner interface {
	Run(environment map[string]string, logger TaskOutputLogger) (*TaskRunnerResult, error)
	LogPlanText() string
}

type TaskRunnerResult struct {
	Output string
}

func ParseTaskRunner(data map[string]interface{}) (TaskRunner, error) {
	for _, taskRunner := range taskRunners {
		taskRunnerData, err := GetDataForKey(taskRunner.Id(), data)
		if err != nil {
			return nil, err
		}
		if taskRunnerData == nil {
			continue
		}

		if IsMap(taskRunnerData) {
			runner, err := taskRunner.Parse(taskRunnerData)
			if err != nil {
				return nil, err
			}

			return runner, nil
		}
	}

	return nil, errors.New(fmt.Sprintf("no runner found"))
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

func TaskRunnerIds() []string {
	ids := make([]string, 0)
	for _, taskRunner := range taskRunners {
		ids = append(ids, taskRunner.Id())
	}

	return ids
}

var WorkflowListTaskRunnersCommand = cli.Command{
	Name:  "runners",
	Usage: "show a list of all available task runners",
	Action: func(context *cli.Context) error {

		if context.Args().Len() == 1 && HasTaskRunner(context.Args().Get(0)) {
			Outputfln(GetTaskRunner(context.Args().Get(0)).Help())
			return cli.Exit("", 0)
		}

		Outputfln("the following runners are available")
		Outputfln("")
		for _, taskRunnerId := range TaskRunnerIds() {
			Outputfln(fmt.Sprintf("  * %s", taskRunnerId))
		}
		Outputfln("")
		Outputfln("to show help for a runner run")
		Outputfln("")
		Outputfln("blcks workflow runners <runner>")

		return cli.Exit("", 0)
	},
}
