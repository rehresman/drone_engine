-- -- -- -- -- -- -- project-name -- -- -- -- -- --
--
-- 
-- 
-- 
-- 
-- 
-- -- -- eman-tcejorp -- -- -- -- -- -- -- -- -- --

engine.name = "ProjectName"

-- delete if randomness isn't needed
math.randomseed(os.time())

shift = false
local pitchMult = 0.1
-- example poll for UI
local ampL = 0
local ampL_poll = nil

-- example function
local function sr(n)
  return 2 ^(n/12)
end
-- example array/table
local semitones = {sr(0),sr(1),sr(2),sr(3),sr(4),sr(5),sr(6),sr(7),sr(8),sr(9),sr(10),sr(11)}


function amp_to_level(amp)
    if amp > 0.0041 then
      amp = clamp(64*amp, 0, 1)
    end
    return math.floor(amp * 15) -- 0..15
end


function init()
  params:add_control("pitch", "pitch", controlspec.new(20, 2000, 'exp', 0, 32.7, 'hz'))

  params:set_action("pitch", function(x)
    engine.freqIn(x)
  end)
  
  
  ampL_poll = poll.set("amp_out_l")
  ampL_poll.callback = function(v)
    --print("amp callback")
  end
  ampL_poll.time = 1/30
  ampL_poll:start()

  redraw()
end

function enc(n, d)
  if shift then
    if n == 1 then
      --params:delta("quantize", d)
    elseif n == 2 then
      --params:delta("rate", d)
    elseif n == 3 then
      --params:delta("cutoff", d*cutoffMult)
    end
  else
    if n == 1 then
      params:delta("pitch", d*pitchMult)
    elseif n == 2 then
      --params:delta("decay", d)
    elseif n == 3 then
      --params:delta("range", d)
    end
  end
  redraw()
end

function k2()
  print('key 2 pressed')
end

function shift_k2()
  print('shift + key 2 pressed')
end

function k3()
  print('key 3 pressed')
  --engine.freqMultIn(1.49830707688)
end

function shift_k3()
  print('shift + key 3 pressed')
  --engine.freqMultIn(1.49830707688)
end


function key(n, z)
  if n == 1 then
    shift = (z == 1)
  elseif n == 2 then
    if shift and z == 1 then
      shift_k2()
    elseif z == 1 then
      k2()
    end
  elseif n == 3 then
    if shift and z == 1 then
      shift_k3()
    elseif z == 1 then
      k(3)
    else
      --default behavior
      --could be nothing
    end
  end
  
  redraw()
end

function get_display_info(slot)
  local key_label, enc_label, value_string
  if not shift then
    if slot == 1 then
      key_label = "(shift)"
      enc_label = "pitch"
      value_string = params:string("pitch")
    elseif slot == 2 then
      key_label = "key 2"
      enc_label = "enc 2"
      --value_string = params:string("decay")
    elseif slot == 3 then
      key_label = "key 3"
      enc_label = "enc 3"
      --value_string = params:string("range")
    end
  else 
    if slot == 1 then
      key_label = "(shift)"
      enc_label = "enc 1 (shift)"
      --value_string = params:string("quantize")
    elseif slot == 2 then
      key_label = "key 2 (shift)"
      enc_label = "enc 2 (shift)"
      --value_string = params:string("rate")
    elseif slot == 3 then
      key_label = "key 3 (shift)"
      enc_label = "enc 3 (shift)"
      --value_string = params:string("cutoff")
    end
  end

  return key_label, enc_label, value_string
end

function draw_control_block(x, y, slot)
  local key_label, enc_label, value_string
  key_label, enc_label, value_string = get_display_info(slot)

  screen.level(15)
  screen.move(x+10, y)
  screen.text(enc_label)

  screen.move(x, y + 13)
  screen.text(key_label)

  screen.level(10)
  screen.move(x+ 23, y + 7)
  screen.text(value_string)
end

function redraw()
  screen.clear()

  draw_control_block(4, 10, 1)

  screen.update()
end

function cleanup()
  if ampL_poll then
    ampL_poll:stop()
  end
end