//
// test config for JStorage. Unit tests may depend on these values
//
var closureInc = 0

var config = {

    "summa.test.int" : 27,
    "summa.test.boolean" : true,
    "summa.test.nested" : {
        "summa.test.subint" : 27
    },
    "summa.test.nestedlist" : [
        {
            "summa.test.int" : 27
        }, {
            "summa.test.boolean" : true
        }
    ],
    "summa.test.closure" : function(){ return ++closureInc },
    "summa.test.listofstrings" : ["one", "two", "three"]

}