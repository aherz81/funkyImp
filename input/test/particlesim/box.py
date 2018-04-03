#!/usr/bin/python

#
# Example boxplot code
#

from pylab import *
import csv

with open('output.csv', 'rb') as csvfile:
	reader = csv.reader(csvfile, delimiter=',')
	figure()
	rows = list(reader)
	data = [map(int, rows[0]), map(int, rows[1])]
	boxplot(data)

	
show()