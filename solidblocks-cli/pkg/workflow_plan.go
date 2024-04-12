package pkg

import (
	"github.com/urfave/cli/v2"
)

func PlanWorkflow(workflow Workflow) error {
	Outputf(TextBoldBlack("global environment variables"))
	Outputf(TextBoldBlack("----------------------------"))
	for _, variable := range workflow.Environment {
		Outputf(variable.LogPlanText())
	}
	if len(workflow.Environment) == 0 {
		Outputf(TextPrimary("<none>"))
	}

	Outputf("")
	Outputf(TextBoldBlack("tasks"))
	Outputf(TextBoldBlack("-----"))
	for _, task := range workflow.Tasks {
		Outputf(task.LogPlanText())
	}

	return nil
}

var WorkflowPlanCommand = cli.Command{
	Name:      "plan",
	Usage:     "explain how the workflow file will be executed",
	ArgsUsage: ParseWorkflowsArgHelp,
	Action: func(context *cli.Context) error {

		workflows, err := ParseWorkflows(context)
		if err != nil {
			return cli.Exit(err.Error(), 1)
		}

		for _, workflow := range workflows {
			err := PlanWorkflow(*workflow)
			if err != nil {
				return cli.Exit(err.Error(), 1)
			}
		}

		return cli.Exit("", 0)

	},
}
