# $Id:$

#
# Ultra simple tcl-script that creates a test-ZIP.
#

if { [llength $argv] == 0 } {
    puts "Usage: makesample <number_of_samples>"
    exit
}

set samples [lindex $argv 0]

if { ![file exists "large$samples"] } {
    file mkdir "large$samples"
}

puts "Creating $samples files..."
for {set i 0} {$i < $samples} {incr i} {
    set fid [open "large$samples/sample$i\.xml" w]
    puts $fid "sample$i"
    close $fid
}

puts "Finished creating $samples files. ZIPping..."
if { [file exists "large$samples\.zip"] } {
    file delete "large$samples\.zip"
}

exec zip -r "large$samples\.zip" "large$samples"
file delete -force "large$samples"

puts "Finished creating large$samples\.zip"
