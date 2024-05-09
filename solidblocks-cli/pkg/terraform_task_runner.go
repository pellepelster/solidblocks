package pkg

import "os"

type TerraformTaskRunner struct {
	Workdir string
}

func (runner TerraformTaskRunner) Run(environment map[string]string, logger TaskOutputLogger) *TaskRunnerResult {

	_, err := os.Stat(runner.Workdir)

	if err != nil {
		return &TaskRunnerResult{Success: false}
	}

	if os.IsNotExist(err) {
		return false, nil
	}
	return false, err

	return nil
}
