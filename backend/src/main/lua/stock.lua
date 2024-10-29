#!lua name=stock
local function buy(keys, args)
  local success_index = 0
  local key_len = #keys
  for i = 1,key_len,1 do
    local decrement = tonumber(args[i])
    local stock = tonumber(redis.call('get',keys[i]) or "0")
    if stock < decrement then
      return keys[i]
    end
  end
  for i = 1,key_len,1 do
    local decrement = tonumber(args[i])
    local stock = tonumber(redis.call('get',keys[i]) or "0")
    redis.call('decrby', keys[i], args[i])
  end
end

redis.register_function('buy', buy)

local function refund(keys,args)
  for i = 1,#keys,1 do
    redis.call('incrby', keys[i], args[i])
  end
end
redis.register_function('refund', refund)