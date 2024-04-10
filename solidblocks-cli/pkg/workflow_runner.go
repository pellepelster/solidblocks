package pkg

import (
	"errors"
	"fmt"
	"github.com/dominikbraun/graph"
	"github.com/urfave/cli/v2"
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

	for _, task := range workflow.Tasks {
		err := g.AddVertex(task.Name)
		if err != nil {
			return err
		}
	}

	for _, task := range workflow.Tasks {
		for _, envVar := range task.Environment {
			if envVar.ValueFrom != nil && len(envVar.ValueFrom.Task) > 0 {
				err := g.AddEdge(task.Name, envVar.ValueFrom.Task)
				if err != nil {
					return err
				}
			}
		}
	}

	sorted, err := graph.TopologicalSort(g)
	Reverse(sorted)

	if err != nil {
		return err
	}

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
					OutputTaskf(taskName, "waiting for output from '%s'", envVar.ValueFrom.Task)
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

					valueFromEnv[envVar.Name] = result.Output
				}
			}

			OutputTaskf(taskName, "running task")
			OutputDivider(taskName)
			resultsChannel <- &WorkflowTaskRunnerResult{TaskName: taskName, RunnerResult: task.Runner.Run(taskName, MergeStringMaps(valueFromEnv, workflow.GetEnvVarsForTask(task)))}
			OutputDivider(taskName)
		}()
	}

	for !hasError && len(results) != len(workflow.Tasks) {
		time.Sleep(100 * time.Millisecond)
	}

	for _, result := range results {
		if !result.RunnerResult.Success {
			return errors.New(fmt.Sprintf("task '%s' failed", result.TaskName))
		}
	}

	return nil
}

var WorkflowRunCommand = cli.Command{
	Name:      "run",
	Usage:     "execute a workflow file",
	ArgsUsage: "manifest",
	Action: func(context *cli.Context) error {

		workflows, err := ParseWorkflows(context)
		if err != nil {
			return cli.Exit(err.Error(), 1)
		}

		for _, workflow := range workflows {
			Outputf("executing workflow '%s'", workflow.Name)
			err := RunWorkflow(*workflow)
			if err != nil {
				return cli.Exit(err.Error(), 1)
			}
		}

		return cli.Exit("", 0)
	},
}
