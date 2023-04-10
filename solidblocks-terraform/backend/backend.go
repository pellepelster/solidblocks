package backend

import (
	"github.com/spf13/cobra"
	"solidblocks.de/terraform/backend/s3"
)

var BackendCmd = &cobra.Command{
	Use:   "backend",
	Short: "Initialize storage backends",
	Long:  `Helper tasks to bootstrap storage backends for terraform`,
}

func init() {
	BackendCmd.AddCommand(s3.BackendS3Cmd)
}
