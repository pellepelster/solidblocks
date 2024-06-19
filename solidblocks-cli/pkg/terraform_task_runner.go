package pkg

import (
	"errors"
	"fmt"
	"os"
)

type TerraformTaskRunner struct {
	Workdir string
}

func (runner TerraformTaskRunner) Run(environment map[string]string, logger TaskOutputLogger) (*TaskRunnerResult, error) {
	_, err := os.Stat(runner.Workdir)

	if err != nil {
		return nil, err
	}

	if os.IsNotExist(err) {
		return nil, errors.New(fmt.Sprint("working directory does not exist: ", runner.Workdir))

	}

	return &TaskRunnerResult{}, nil
}
