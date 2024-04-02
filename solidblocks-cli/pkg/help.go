package pkg

import (
	"embed"
	markdown "github.com/MichaelMure/go-term-markdown"
)

func RenderHelp(fs embed.FS, helpFile string) string {
	source, err := fs.ReadFile(helpFile)
	if err != nil {
		panic(err)
	}

	return string(markdown.Render(string(source), 120, 0))
}
