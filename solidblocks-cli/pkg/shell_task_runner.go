package pkg

import (
	"bytes"
	"embed"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"strings"
)

type ShellTaskRunnerRegistration struct {
}

type ShellTaskRunner struct {
	Command []string
	Script  []string
	Workdir string
}

func (_ ShellTaskRunnerRegistration) Id() string {
	return "shell"
}

func (_ ShellTaskRunnerRegistration) Help() string {
	return RenderHelp(shellTaskRunnerHelp, "shell_task_runner_help.md")
}

func (_ ShellTaskRunnerRegistration) Parse(data map[string]interface{}) (TaskRunner, error) {

	workdir := GetOptionalStringByKey("workdir", data)

	script, err := GetAsStringList("script", data)
	if err == nil {
		return ShellTaskRunner{Script: script, Workdir: workdir}, nil
	}

	command, err := GetAsStringList("command", data)
	if err == nil {
		return ShellTaskRunner{Command: command, Workdir: workdir}, nil
	}

	return nil, errors.New("either a command or a script has to be provided")
}

func (runner ShellTaskRunner) LogPlanText() string {
	if runner.Command != nil {
		return fmt.Sprintf("execute command '%s' in workding dir '%s'", TextSecondary(strings.Join(runner.Command, " ")), TextSecondary(runner.Workdir))
	} else {
		return fmt.Sprintf("execute script '%s' in workding dir '%s'", TextSecondary(strings.Join(runner.Script, " ")), TextSecondary(runner.Workdir))
	}
}

func (runner ShellTaskRunner) Run(environment map[string]string, logger TaskOutputLogger) *TaskRunnerResult {

	var cmd *exec.Cmd

	command := make([]string, 0)

	if runner.Command != nil {
		command = runner.Command
	}

	if len(runner.Script) > 0 {
		command = append([]string{"/bin/sh"}, runner.Script...)
	}

	cmd = exec.Command(command[0], command[1:]...)

	env := make([]string, 0)
	for envKey, envValue := range environment {
		env = append(env, fmt.Sprintf("%s=%s", envKey, envValue))
	}
	cmd.Env = env
	cmd.Dir = runner.Workdir

	stdout := bytes.NewBufferString("")
	stderr := bytes.NewBufferString("")

	w1 := &TaskWriter{logger, os.Stdout}
	w2 := &TaskWriter{logger, os.Stderr}

	cmd.Stdout = io.MultiWriter(w1, stdout)
	cmd.Stderr = io.MultiWriter(w2, stderr)

	err := cmd.Start()
	if err != nil {
		logger(err.Error())
		return &TaskRunnerResult{
			Success: false,
		}
	}

	err = cmd.Wait()
	if err != nil {
		logger(err.Error())
		return &TaskRunnerResult{
			Success: false,
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
