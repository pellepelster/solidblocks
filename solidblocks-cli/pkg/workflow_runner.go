package pkg

import (
	"errors"
	"fmt"
	"github.com/dominikbraun/graph"
	"github.com/urfave/cli/v2"
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
	tasks, err := createTaskGraph(workflow)
	if err != nil {
		return err
	}

	results := make([]*WorkflowTaskRunnerResult, 0)

	for _, taskName := range tasks {
		task := workflow.getTask(taskName)

		valueFromEnv := make(map[string]string)

		for _, envVar := range task.Environment {
			if envVar.ValueFrom != nil && len(envVar.ValueFrom.Task) > 0 {
				result, err := ResultForTask(results, envVar.ValueFrom.Task)
				if err != nil {
					return err
				}

				if envVar.Transform != nil && len(envVar.Transform.Json) > 0 {
					transformResult, err := TransformJson(result.Output, envVar.Transform.Json)
					if err != nil {
						return err
					}

					valueFromEnv[envVar.Name] = transformResult
				} else {
					valueFromEnv[envVar.Name] = result.Output
				}
			}
		}

		var logger = func(s string) {
			Output(s)
			/*
				if strings.Contains(s, "\n") {
					scanner := bufio.NewScanner(strings.NewReader(s))
					for scanner.Scan() {
						Outputfln("%s: %s", JustifyTaskName(taskName, maxTaskNameLength), scanner.Text())
					}
				} else {
					Outputf("%s: %s", JustifyTaskName(taskName, maxTaskNameLength), s)
				}*/
		}

		Outputln("")
		Outputfln("starting task %s", TextPrimary(taskName))
		OutputDivider()
		result := task.Runner.Run(MergeStringMaps(valueFromEnv, workflow.GetEnvVarsForTask(task)), logger)
		results = append(results, &WorkflowTaskRunnerResult{TaskName: taskName, RunnerResult: result})
		Outputln("")
		OutputDivider()

		if result.Success {
			Outputfln("task %s %s", TextPrimary(taskName), TextSuccess("finished"))
		} else {
			Outputfln("task %s %s", TextPrimary(taskName), TextAlert("failed"))
			return errors.New(fmt.Sprintf("task failed"))
		}
	}

	return nil
}

func createTaskGraph(workflow Workflow) ([]string, error) {
	g := graph.New(graph.StringHash, graph.Directed(), graph.PreventCycles())

	for _, task := range workflow.Tasks {
		err := g.AddVertex(task.Name)
		if err != nil {
			return nil, err
		}
	}

	for _, task := range workflow.Tasks {
		for _, envVar := range task.Environment {
			if envVar.ValueFrom != nil && len(envVar.ValueFrom.Task) > 0 {

				_, err := g.Edge(task.Name, envVar.ValueFrom.Task)
				if errors.Is(err, graph.ErrEdgeNotFound) {
					err := g.AddEdge(task.Name, envVar.ValueFrom.Task)
					if err != nil {
						return nil, err
					}
				}
			}
		}
	}

	tasks, err := graph.TopologicalSort(g)
	if err != nil {
		return nil, err
	}
	Reverse(tasks)

	return tasks, nil
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
				return cli.Exit(fmt.Sprintf("workflow failed: %s", err.Error()), 1)
			}
		}

		return cli.Exit("", 0)
	},
}
