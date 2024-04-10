package pkg

import (
	"io"
)

type TaskWriter struct {
	taskName string
	writer   io.Writer
}

func (e TaskWriter) Write(p []byte) (int, error) {
	Outputf("%s: %s", e.taskName, string(p))
	return len(p), nil
}
