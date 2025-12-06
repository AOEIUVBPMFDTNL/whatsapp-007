local key =  KEYS[1];
local ttl = redis.call('TTL', key)
local current_value = redis.call('GET', key)
local increment_value = tonumber(ARGV[1])
local expire_time = tonumber(ARGV[2])

if current_value then
    current_value = tonumber(current_value)
    current_value = current_value + increment_value
    redis.call('SET', key, tonumber(current_value))
    if ttl > 0 then
        redis.call('EXPIRE', key, ttl)
    end
else
    current_value = increment_value
    redis.call('SET', key, tonumber(current_value))
    redis.call('EXPIRE', key, tonumber(expire_time))
end
return current_value
