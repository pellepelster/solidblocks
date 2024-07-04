import time


def wait_until(predicate, timeout, period=2, *args, **kwargs):
    end = time.time() + timeout
    while time.time() < end:
        if predicate(*args, **kwargs):
            return True
        time.sleep(period)
    return False


