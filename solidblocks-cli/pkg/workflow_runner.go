package pkg

import (
	"errors"
	"fmt"
	"github.com/dominikbraun/graph"
	"github.com/urfave/cli/v2"
	"strings"
	"time"
)

type WorkflowTaskRunnerResult struct {
	TaskName     string
	RunnerResult *TaskRunnerResult
}

func ResultForTask(results []*WorkflowTaskRunnerResult, taskName string) (*TaskRunnerResult, error) {
	for _, result := range results {
		if result.TaskName == taskName {
			return result.RunnerResult, nil
		}
	}
	return nil, errors.New(fmt.Sprintf("could not get output for task '%s', task not found", taskName))
}

func RunWorkflow(workflow Workflow) error {
	g := graph.New(graph.StringHash, graph.Directed(), graph.PreventCycles())

	maxTaskNameLength := 0
	for _, task := range workflow.Tasks {
		if len(task.Name) > maxTaskNameLength {
			maxTaskNameLength = len(task.Name)
		}

		err := g.AddVertex(task.Name)
		if err != nil {
			return err
		}
	}

	for _, task := range workflow.Tasks {
		for _, envVar := range task.Environment {
			if envVar.ValueFrom != nil && len(envVar.ValueFrom.Task) > 0 {

				_, err := g.Edge(task.Name, envVar.ValueFrom.Task)
				if errors.Is(err, graph.ErrEdgeNotFound) {
					err := g.AddEdge(task.Name, envVar.ValueFrom.Task)
					if err != nil {
						return err
					}
				}
			}
		}
	}

	sorted, err := graph.TopologicalSort(g)
	if err != nil {
		return err
	}
	Reverse(sorted)

	resultsChannel := make(chan *WorkflowTaskRunnerResult)
	results := make([]*WorkflowTaskRunnerResult, 0)
	hasError := false

	go func() {
		for result := range resultsChannel {
			if !result.RunnerResult.Success {
				hasError = true
			}
			results = append(results, result)
		}
	}()

	for _, taskName := range sorted {
		taskName := taskName
		go func() {
			task := workflow.getTask(taskName)

			for _, envVar := range task.Environment {
				if envVar.ValueFrom != nil && len(envVar.ValueFrom.Task) > 0 {
					OutputTaskf(JustifyTaskName(taskName, maxTaskNameLength), "waiting for output from %s", TextPrimary(envVar.ValueFrom.Task))
					for true {
						_, err := ResultForTask(results, envVar.ValueFrom.Task)
						if err == nil {
							break
						}
						time.Sleep(100 * time.Millisecond)
					}

				}
			}

			valueFromEnv := make(map[string]string)

			for _, envVar := range task.Environment {
				if envVar.ValueFrom != nil && len(envVar.ValueFrom.Task) > 0 {
					result, err := ResultForTask(results, envVar.ValueFrom.Task)

					if err != nil {
						resultsChannel <- &WorkflowTaskRunnerResult{TaskName: taskName, RunnerResult: &TaskRunnerResult{false, ""}}
					}

					if envVar.Transform != nil && len(envVar.Transform.Json) > 0 {
						transformResult, err := TransformJson(result.Output, envVar.Transform.Json)
						if err != nil {
							resultsChannel <- &WorkflowTaskRunnerResult{TaskName: taskName, RunnerResult: &TaskRunnerResult{false, ""}}
						}
						valueFromEnv[envVar.Name] = transformResult
					} else {
						valueFromEnv[envVar.Name] = result.Output

					}
				}
			}

			OutputTaskf(JustifyTaskName(taskName, maxTaskNameLength), "starting task %s", TextPrimary(taskName))
			OutputDividerTask(JustifyTaskName(taskName, maxTaskNameLength), "=")
			var logger = func(s string) {
				OutputTaskf(JustifyTaskName(taskName, maxTaskNameLength), s)
			}

			result := task.Runner.Run(MergeStringMaps(valueFromEnv, workflow.GetEnvVarsForTask(task)), logger)
			resultsChannel <- &WorkflowTaskRunnerResult{TaskName: taskName, RunnerResult: result}
			OutputDividerTask(JustifyTaskName(taskName, maxTaskNameLength), "=")

			if result.Success {
				OutputTaskf(JustifyTaskName(taskName, maxTaskNameLength), "task %s %s", TextSuccess(taskName), TextSuccess("failed"))
			} else {
				OutputTaskf(JustifyTaskName(taskName, maxTaskNameLength), "task %s %s", TextPrimary(taskName), TextAlert("failed"))
			}
		}()
	}

	for !hasError && len(results) != len(workflow.Tasks) {
		time.Sleep(100 * time.Millisecond)
	}

	for _, result := range results {
		if !result.RunnerResult.Success {
			return errors.New(fmt.Sprintf("%s", TextAlert("workflow failed")))
		}
	}

	return nil
}

var WorkflowRunCommand = cli.Command{
	Name:      "run",
	Usage:     "execute a workflow file",
	ArgsUsage: ParseWorkflowsArgHelp,
	Action: func(context *cli.Context) error {

		workflows, err := ParseWorkflows(context)
		if err != nil {
			return cli.Exit(err.Error(), 1)
		}

		for _, workflow := range workflows {
			err := RunWorkflow(*workflow)
			if err != nil {
				return cli.Exit(err.Error(), 1)
			}
		}

		return cli.Exit("", 0)
	},
}

func JustifyTaskName(taskName string, maxTaskNameLength int) string {
	return strings.Repeat(" ", maxTaskNameLength-len(taskName)) + taskName
}
