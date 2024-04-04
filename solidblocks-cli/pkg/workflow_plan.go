package pkg

import (
	"fmt"
	"github.com/urfave/cli/v2"
)

func PlanWorkflow(workflow Workflow) error {
	Outputf("global environment variables")
	for _, variable := range workflow.Environment {
		Outputf(fmt.Sprintf("  %s", variable.LogPlanText()))
	}
	return nil
}

var WorkflowPlanCommand = cli.Command{
	Name:      "plan",
	Usage:     "explain how the workflow file will be executed",
	ArgsUsage: "manifest",
	Action: func(context *cli.Context) error {

		workflows, err := ParseWorkflows(context)
		if err != nil {
			return cli.Exit(err.Error(), 1)
		}

		for _, workflow := range workflows {
			Outputf("simulating workflow '%s'", workflow.Name)
			err := PlanWorkflow(*workflow)
			if err != nil {
				return cli.Exit(err.Error(), 1)
			}
		}

		return cli.Exit("", 0)

	},
}
