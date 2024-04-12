package pkg

import (
	"io"
)

type TaskWriter struct {
	logger TaskOutputLogger
	writer io.Writer
}

func (e TaskWriter) Write(p []byte) (int, error) {
	e.logger(string(p))
	return len(p), nil
}
