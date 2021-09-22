# ms.csv.column.header.index

[back to summary](https://github.com/linkedin/data-integration-library/blob/master/docs/parameters/summary.md)

## Category

[csv extractor](https://github.com/linkedin/data-integration-library/blob/master/docs/parameters/csv-extractor-parameters.md)

## Type

integer

## Required

No

## Default value

0

## Related 
- [job property: ms.csv.column.header](https://github.com/linkedin/data-integration-library/blob/master/docs/parameters/ms.csv.column.header.md)
- [job property: ms.csv.skip.lines](https://github.com/linkedin/data-integration-library/blob/master/docs/parameters/ms.csv.skip.lines.md)

## Description

ms.csv.column.header.index specifies the 0-based row index of the header columns if they are available.

CSV files may have 1 or more descriptive lines before the actual data. These descriptive lines, 
including the column header line, should be skipped. see [ms.csv.skip.lines](https://github.com/linkedin/data-integration-library/blob/master/docs/parameters/ms.csv.skip.lines.md)

Note the column header line can be in any place of the skipped lines 

## Example 1: header row at the front

name1, name2, name3 (header)
blah, blah, blah
val1, va2, va3

Job configuration would be:

`ms.csv.column.header = true`

`ms.csv.column.header.index = 0` 

`ms.csv.skip.lines = 3`

## Example 2: header row at the end of skip lines

blah, blah, blah
val1, va2, va3
name1, name2, name3 (header)

Job configuration would be:

`ms.csv.column.header = true`

`ms.csv.column.header.index = 2`

`ms.csv.skip.lines = 3`