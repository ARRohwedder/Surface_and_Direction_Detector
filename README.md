# Surface_and_Direction_Detector
Calculates extensions from cells.

Installation: Save the .class file in the plugin/analyse folder of ImageJ or use the compile and run option from the plugin tab in ImageJ on the .java file. Also requires to save the sketch.jpg file in the same folder.

Usage:
Works with 8 bit gray scale images only.
After starting the plugin a windows pops up with some settings that can be adjusted:
Grey threshold at the top allows to change threshold for cell detection (default = 20).
Percentage for major protrusions allows to adjust the theoretical amount in percent a major protrusion can occupy of the cell outline (default = 30).
Percentage for minor protrusions allwos to adjust the theoretical amount in percent a minor protrusion or filopodia of the cell outline (default = 2).
Image coverage of cell allows to change the percentage a cell might occupy in the whole image area (default = 30).

Output:
3 results windows will open after successful quantification: Major protrusions lists all identified major protrusions according to the pre-adjusted settings including lengths. Minor protrusions of filopodia lists all identified filopodia according to the pre-adjusted settings with their lenghts. The 3rd results window will sum up (counts) the protrusions by type.
Additional a image window will pop up showing the outline of the measured cell as a control.
