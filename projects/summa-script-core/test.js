/*print("Hello from js!\n");

print ("Jumpr:\n");
for (s in Jumpr) {
    print ("\t* " + s + " : ");
    for (sub in Jumpr[s]) {
        print (sub + " ");
    }
    print ("\n");
}

print ("\nTest: " + Jumpr.Pingable.Foobar + "\n");*/

/* Imports */
Pingable = Jumpr.Pingable;

conf = {
    __class__ : Pingable.Foobar,
    ping : "JSPing"
};

p = create(conf);
print ("Got pong: " + p.ping("hello?") + "\n");
