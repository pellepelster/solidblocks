package pkg

import (
	"errors"
	"fmt"
	"github.com/charmbracelet/log"
	"github.com/dominikbraun/graph"
	"github.com/urfave/cli/v2"
	"time"
)

type WorkflowTaskRunnerResult struct {
	TaskName     string
	RunnerResult *TaskRunnerResult
}

func RunWorkflow(workflow Workflow) error {
	g := graph.New(graph.StringHash, graph.Directed(), graph.PreventCycles())

	for _, task := range workflow.Tasks {
		err := g.AddVertex(task.Name)
		if err != nil {
			return err
		}
	}

	sorted, err := graph.TopologicalSort(g)
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
			log.Infof("running task '%s'", taskName)
			resultsChannel <- &WorkflowTaskRunnerResult{TaskName: taskName, RunnerResult: workflow.getTask(taskName).Runner.Run(GetEnvironment(workflow))}
		}()
	}

	for !hasError && len(results) != len(workflow.Tasks) {
		time.Sleep(2 * time.Second)
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
			log.Infof("executing workflow '%s'", workflow.Name)
			err := RunWorkflow(*workflow)
			if err != nil {
				return cli.Exit(err.Error(), 1)
			}
		}

		return cli.Exit("", 0)
	},
}
