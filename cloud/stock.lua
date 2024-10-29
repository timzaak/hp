#!lua name=stock
local function buy(keys, args)
  local success_index = 0
  local key_len = #keys - 1
  local transaction_status = redis.call('get', keys[key_len])
  if transaction_status ~= nil then
      return 'already ' + transaction_status
  end
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
  redis.call('set', keys[key_len], 'prepare')
  redis.call('expire', keys[key_len], '3600')
end

redis.register_function('buy', buy)

local function refund(keys,args)
  for i = 1,#keys,1 do
    redis.call('incrby', keys[i], args[i])
  end
end

redis.register_function('refund', refund)

local function rollback_buy(keys, args)
  local transaction_status = redis.call('get', keys[key_len])
    if transaction_status ~= 'prepare' then
        return
    end
  for i = 1,key_len[1],1 do
    redis.call('incrby', keys[i], args[i])
  end
  redis.call('set', keys[key_len], 'cancel')
  redis.call('expire', keys[key_len], '3600')
end

redis.register_function('rollback_buy', rollback_buy)
