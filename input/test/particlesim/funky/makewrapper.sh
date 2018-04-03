#!/bin/bash

COMMAND="
	KEY=\"A\";
	while [ \"\$KEY\" == \"A\" ]; do
		export FUNKY_LIB_PATH=/home/aherz/funky/funkyimp;
		make release;
		`pwd`/tmp/cur.run;
		echo ' ';
		echo 'Press ENTER to close this terminal.';
		read KEY;
		clear;
	done"

gnome-terminal --working-directory `pwd` -t "Making install in `pwd`" -x bash -c "$COMMAND"
