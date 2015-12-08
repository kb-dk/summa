Test-data for the ParentChildTest release test.

one_book.xml: Parent- and child-pointers to non-existing records.

parent_book1: Pointers to child1 (existing), child2 (existing) and child3 (non-existing)
child_book1: Pointer to parent1
child_book2: No pointer
child_book4: Pointer to parent1
subchild_book1: Pointer to child_book4

Expected result: 2 indexed records - one_book and parent_book.
one_book contains only data from one_book.xml.
parent_book contains data from child_book1, child_book2, child_book4 and
subchild_book1.