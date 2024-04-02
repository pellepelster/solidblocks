package main

import (
	"github.com/charmbracelet/log"
	"github.com/pellepelster/blcks/pkg"
	"github.com/urfave/cli/v2"
	"os"
)

func main() {
	log.SetReportTimestamp(false)

	app := &cli.App{
		Name:  "blcks",
		Usage: "cli for Solidblocks components",
		Commands: []*cli.Command{
			&pkg.WorkflowCommand,
		},
	}

	if err := app.Run(os.Args); err != nil {
		log.Fatal(err)
	}
}
