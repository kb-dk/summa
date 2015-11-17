#!/usr/bin/tclsh
# $Id: $

#
# Generates sample data for hierarchical faceting tests.
# Used primarily for testing SOLR-64, SOLR-792 and SOLR-2412
#
# Note: SOLR-64 is single-path only so this can only be compared
# to SOLR-792 and SOLR-2412 if all elements are 1. If all elements
# are 1 and -64 is specified, 
#

proc usage {} {
    puts ""
    puts "Usage: facet_samples.tcl <-64> docs elements* <-u uniques*>"
    puts ""
    puts "-64       Special switch for generating output meant for SOLR-64."
    puts "          Setting this to true adds the field 'levels_h' to the output"
    puts "docs:     The number of documents to generate facet values for"
    puts "elements: The number of elements at a given depth in the path for any document"
    puts "uniques:  The number of unique elements at a given depth"
    puts "          If no uniques are given, all tags at alle levels will be unique"
    puts ""
    puts "Sample 1: facet_samples.tcl 1 2"
    puts "doc 1, path_ss=t1, path_ss=t2"
    puts ""
    puts "Sample 2: facet_samples.tcl 2 false 2 3"
    puts "doc 1, path_ss=p1/t1, path_ss=p1/t2, path_ss=p1/t3,"
    puts "       path_ss=p2/t1, path_ss=p2/t2, path_ss=p2/t3"
    puts "doc 2, path_ss=p1/t1, path_ss=p1/t2, path_ss=p1/t3,"
    puts "       path_ss=p2/t1, path_ss=p2/t2, path_ss=p2/t3"
    puts ""
    puts "Sample 3: facet_samples.tcl 3 true 2"
    puts "doc 1, path_ss=t1_d1, path_ss=t2_d1"
    puts "doc 2, path_ss=t1_d2, path_ss=t2_d2"
    puts "doc 3, path_ss=t1_d3, path_ss=t2_d3"
    puts ""
    exit
}

proc incgetcount { level } {
    global uniques
    global counters
   
    set u [lindex $counters $level]
    set result $u
    incr u
    if { $u > [lindex $uniques $level] } {
        set u 1
    }
    lset counters $level $u
    return $result
}

# 2 2 -u 2 3 ->  L0_T1/L1_T1, L0_T1/L1_T2, L0_T2/L1_T3, L0_T2/L1_T1
proc getexp { level path } {
    global elements

    set result {}
    if { $level == [llength $elements] } {
        return $path
    }
    if { $level > 0 } {
        set path "$path/"
    }
 
    for {set e 0} {$e < [lindex $elements $level]} {incr e} {
        set val "L$level\_T[incgetcount $level]"
        set result [concat $result [getexp [expr $level + 1] "$path$val"]]
    }
    return $result
}

# 2 2 -u 2 3 ->  L0_T1, L0_T1, L1_T1, L1_T2, L1_T3, L1_T1, L1_T2, L1_T3
proc get792 { } {
    global elements

    set result {}
    set combos 1
    for {set level 0} {$level < [llength $elements]} {incr level} {
        set c [lindex $elements $level]
        set combos [expr $combos * $c]
        
        set path "L$level\_T"
        for {set tag 0} {$tag < $combos} {incr tag} {
            lappend result "$path[incgetcount $level]"
        }
    }
    return $result
}

proc print { list } {
    foreach element $list {
        puts -nonewline ",$element"
    }
}

proc makecounters { } { 
    global elements

    set counters {}
    for {set c 0} {$c < [llength $elements]} {incr c} {
        lappend counters 1
    }
    return $counters
}


# Parse arguments
if { [llength $argv] < 2 } {
    puts stderr "Too few arguments\n"
    usage
}
if { [lindex $argv 0] == "-64" } {
    set solr64 true
    set argv [lrange $argv 1 end]
} else {
    set solr64 false
}
set docs [lindex $argv 0]
set u [lsearch $argv "-u"]
if { $u > -1 } {
    set uniques [lrange $argv [expr $u + 1] end]
    set elements [lrange $argv 1 [expr $u - 1]]
    if { !([llength $elements] == [llength $uniques]) } {
        puts stderr "#elements must be equal to #uniques."
        puts stderr "Got [llength $elements] elements ($elements) and [llength $uniques] uniques ($uniques) in arguments '$argv'"
        usage
    }
} else {
    set elements [lrange $argv 1 end]
    set uniques {}
    for {set u 0} {$u < [llength $elements]} {incr u} {
        lappend uniques 2147483648
    }
}
if { $solr64 } {
    foreach element $elements {
        if { !($element == 1) } {
            puts stderr "When '-64' is specified, all elements must be 1. Got elements $elements"
            usage
        }
    }
}


# Write header
set paths 1
foreach element $elements {
    set paths [expr $paths * $element]
}
puts -nonewline "id"
if { $solr64 } {
    puts -nonewline ",levels_h"
}
for {set i 0} {$i < $paths} {incr i} {
    puts -nonewline ",path_ss"
}
set combos 1
for {set l 0} {$l < [llength $elements]} {incr l} {
    set combos [expr $combos * [lindex $elements $l]]
    for {set m 0} {$m < $combos} {incr m} {
        if { [lindex $elements $l] == 1 } {
            puts -nonewline ",level$l\_s"
        } else {
            puts -nonewline ",level$l\_ss"
        }
    }
}
puts ""


# Write paths

set countersexp [makecounters]
set counters792 [makecounters]
for {set doc 1} {$doc <= $docs} {incr doc} {
    puts -nonewline $doc
#    set values [calculate_values $elements $uniques $counters]
#    set counters [makepath "" $solr64 $doc 0 $elements $uniques $counters]
    set counters $countersexp
    set exp [getexp 0 {}]
    set countersexp $counters
    if { $solr64 } {
        print $exp
    }
    print $exp

    set counters $counters792
    print [get792]
    set counters792 $counters
    puts ""
}
