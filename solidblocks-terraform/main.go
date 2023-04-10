package main

import (
	"github.com/spf13/cobra"
	"os"
	"solidblocks.de/terraform/backend"
)

var rootCmd = &cobra.Command{
	Use:   "solidblocks-terraform",
	Short: "Helper tasks for terraform",
}

func Execute() {
	err := rootCmd.Execute()
	if err != nil {
		os.Exit(1)
	}
}

func init() {
	rootCmd.AddCommand(backend.BackendCmd)
}

func main() {
	rootCmd.Execute()
}
