package pkg

import (
	"github.com/jedib0t/go-pretty/v6/table"
	"github.com/urfave/cli/v2"
	"os"
)

func OrNone(s string) string {
	if len(s) > 0 {
		return s
	} else {
		return "<none>"
	}
}

func WorkflowShow(workflow Workflow) error {
	OutputHeader("global environment variables")

	t := table.NewWriter()
	t.SetOutputMirror(os.Stdout)
	t.SetStyle(table.StyleLight)
	t.SetAllowedRowLength(TermWidth())
	t.AppendHeader(table.Row{"Name", "Description", "Source", "Value"})

	for _, variable := range workflow.Environment {
		value, _ := EnvironmentResolve(variable)
		t.AppendRows([]table.Row{
			{variable.Name, OrNone(variable.Description), EnvironmentResolveHelp(variable), OrNone(value)},
		})
	}

	t.AppendSeparator()
	t.Render()

	if len(workflow.Environment) == 0 {
		Outputfln(TextPrimary("<none>"))
	}

	Outputfln("")
	OutputHeader("tasks")
	for _, task := range workflow.Tasks {
		Outputfln(task.LogPlanText())
	}

	return nil
}

var WorkflowShowAction = func(context *cli.Context) error {

	workflows, err := ParseWorkflows(context)
	if err != nil {
		return cli.Exit(err.Error(), 1)
	}

	for _, workflow := range workflows {
		err := WorkflowShow(*workflow)
		if err != nil {
			return cli.Exit(err.Error(), 1)
		}
	}

	return cli.Exit("", 0)

}

var WorkflowShowCommand = cli.Command{
	Name:      "show",
	Usage:     "show available tasks and variables",
	ArgsUsage: ParseWorkflowsArgHelp,
	Action:    WorkflowShowAction,
}
