def outer(num1):
    def inner_increment(num1):
        if num1 == 0:
            return 0

        for k in range(10, 0, -1):
            print k
        return num1 + 1

    num2 = inner_increment(num1)
    print(num1, num2)
