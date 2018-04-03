all:
	latexmk -pdf -bibtex main.tex
	latexmk -pdf presentation/presentation.tex

.PHONY: clean

clean:
	rm -f *.out *.log *.bbl *.blg *.lof *.aux *.toc *.glo *.fls *.fdb_latexmk

presentation:
	latexmk -pdf presentation/presentation.tex

open:
	latexmk -pdf -bibtex main.tex
	open main.pdf
