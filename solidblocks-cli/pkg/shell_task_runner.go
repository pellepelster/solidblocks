package pkg

import (
	"bytes"
	"embed"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
)

type ShellTaskRunnerRegistration struct {
}

type ShellTaskRunner struct {
	Command []string
	Script  string
}

func (_ ShellTaskRunnerRegistration) Id() string {
	return "shell"
}

func (_ ShellTaskRunnerRegistration) Help() string {
	return RenderHelp(shellTaskRunnerHelp, "shell_task_runner_help.md")
}

func (_ ShellTaskRunnerRegistration) Parse(data map[string]interface{}) (TaskRunner, error) {
	script, err := GetStringKey("script", data)
	if err == nil {
		return ShellTaskRunner{Script: script}, nil
	}

	command, err := GetAsStringList("command", data)
	if err == nil {
		return ShellTaskRunner{Command: command}, nil
	}

	return nil, errors.New("either a command or a script has to be provided")
}

func (runner ShellTaskRunner) Run(environment map[string]string) *TaskRunnerResult {

	var cmd *exec.Cmd
	if runner.Command != nil {
		cmd = exec.Command(runner.Command[0], runner.Command[1:]...)
	}

	if len(runner.Script) > 0 {
		cmd = exec.Command("/bin/sh", runner.Script)
	}

	env := make([]string, 0)
	for envKey, envValue := range environment {
		env = append(env, fmt.Sprintf("%s=%s", envKey, envValue))
	}

	cmd.Env = env

	stdout := bytes.NewBufferString("")
	stderr := bytes.NewBufferString("")

	cmd.Stdout = io.MultiWriter(os.Stdout, stdout)
	cmd.Stderr = io.MultiWriter(os.Stderr, stderr)

	err := cmd.Start()
	if err != nil {
		if err != nil {
			return &TaskRunnerResult{
				Success: false,
				Output:  err.Error(),
			}
		}
	}

	err = cmd.Wait()

	if err != nil {
		return &TaskRunnerResult{
			Success: false,
			Output:  err.Error(),
		}
	}

	return &TaskRunnerResult{
		Success: true,
		Output:  stdout.String(),
	}
}

func init() {
	RegisterTaskRunner(ShellTaskRunnerRegistration{})
}

//go:embed shell_task_runner_help.md
var shellTaskRunnerHelp embed.FS
