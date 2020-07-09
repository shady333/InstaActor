from fp.fp import FreeProxy


proxies = FreeProxy(country_id=['UA'], timeout=1).get()
print(proxies.split("//")[1])


# print('Hello')