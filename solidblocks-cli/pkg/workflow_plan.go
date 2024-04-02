package pkg

import (
	"fmt"
	"github.com/charmbracelet/log"
	"github.com/urfave/cli/v2"
)

func PlanWorkflow(workflow Workflow) error {
	log.Infof("global environment variables")
	for _, variable := range workflow.Environment {
		log.Infof(fmt.Sprintf("  %s", variable.LogPlanText()))
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
			log.Infof("simulating workflow '%s'", workflow.Name)
			err := PlanWorkflow(*workflow)
			if err != nil {
				return cli.Exit(err.Error(), 1)
			}
		}

		return cli.Exit("", 0)

	},
}
