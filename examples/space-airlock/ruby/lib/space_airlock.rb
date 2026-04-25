# frozen_string_literal: true

require 'json'
require 'fileutils'
require 'rspec/expectations'

class SpaceAirlock
  include RSpec::Matchers

  def initialize
    reset
  end

  def reset
    @state = default_state
  end

  def crew_member_inside(name)
    set_occupant(name, 'crew')
  end

  def visitor_inside
    set_occupant('visitor', 'visitor')
  end

  def wearing_suit(name)
    set_occupant(name, 'crew')
    set_suit(true)
  end

  def not_wearing_suit(name)
    set_occupant(name, 'crew')
    set_suit(false)
  end

  def valid_badge(name)
    set_occupant(name, 'crew')
    set_badge(true)
  end

  def visitor_invalid_badge
    set_occupant('visitor', 'visitor')
    set_badge(false)
  end

  def door_state(door, state)
    key = door_key(door)
    case state
    when 'open', 'closed'
      @state[key]['position'] = state
    when 'locked', 'unlocked'
      @state[key]['lock'] = state
    else
      raise "Unknown door state: #{state}"
    end
  end

  def chamber_pressure(pressure)
    @state['pressure'] = pressure
  end

  def emergency_override_engaged
    @state['override'] = true
  end

  def request_exit(_name)
    occupant = @state['occupant'] || {}
    return deny('Authorization required') unless occupant['badge_valid?']
    return deny('Suit required') unless occupant['suit?']

    @state['request_status'] = 'approved'
    @state['message'] = nil
    @state['pressure'] = 'depressurized'
    @state['outer_door']['lock'] = 'unlocked'
    @state['inner_door']['lock'] = 'locked'
    @state['status'] = 'cycle complete'
  end

  def open_door(door)
    key = door_key(door)
    if @state['override']
      @state[key]['lock'] = door == 'inner' ? 'unlocked' : 'locked'
      @state['message'] = door == 'inner' ? 'Emergency override active' : 'Outer door lock preserved'
    elsif door == 'inner'
      if @state['pressure'] == 'depressurized'
        @state[key]['lock'] = 'locked'
        @state['message'] = 'Repressurize first'
      else
        @state[key]['lock'] = 'unlocked'
        @state['message'] = nil
      end
    elsif @state['pressure'] == 'pressurized'
      @state[key]['lock'] = 'locked'
      @state['message'] = 'Depressurize first'
    else
      @state[key]['lock'] = 'unlocked'
      @state['message'] = nil
    end
  end

  def depressurization_commanded
    if @state['inner_door']['position'] == 'open'
      @state['pressure'] = 'pressurized'
      @state['message'] = 'Close inner door'
    else
      @state['pressure'] = 'depressurized'
      @state['message'] = nil
    end
  end

  def repressurization_commanded
    if @state['outer_door']['position'] == 'open'
      @state['pressure'] = 'depressurized'
      @state['message'] = 'Close outer door'
    else
      @state['pressure'] = 'pressurized'
      @state['message'] = nil
    end
  end

  def chamber_should_depressurize
    expect(@state['pressure']).to eq('depressurized')
  end

  def chamber_should_remain(pressure)
    expect(@state['pressure']).to eq(pressure)
  end

  def door_should_unlock(door)
    expect(@state[door_key(door)]['lock']).to eq('unlocked')
  end

  def door_should_remain_locked(door)
    expect(@state[door_key(door)]['lock']).to eq('locked')
  end

  def request_should_be_denied
    expect(@state['request_status']).to eq('denied')
  end

  def system_should_display(message)
    expect(@state['message']).to eq(message)
  end

  def airlock_status_should_be(status)
    expect(status_for).to eq(status)
  end

  private

  def default_state
    {
      'occupant' => nil,
      'pressure' => 'pressurized',
      'inner_door' => { 'position' => 'closed', 'lock' => 'locked' },
      'outer_door' => { 'position' => 'closed', 'lock' => 'locked' },
      'override' => false,
      'message' => nil,
      'request_status' => nil,
      'status' => nil
    }
  end

  def ensure_occupant
    @state['occupant'] ||= { 'name' => nil, 'kind' => 'crew', 'suit?' => false, 'badge_valid?' => false }
  end

  def set_occupant(name, kind)
    current = @state['occupant'] || {}
    @state['occupant'] = {
      'name' => name,
      'kind' => kind,
      'suit?' => !!current['suit?'],
      'badge_valid?' => !!current['badge_valid?']
    }
  end

  def set_suit(suit)
    ensure_occupant
    @state['occupant']['suit?'] = suit
  end

  def set_badge(badge)
    ensure_occupant
    @state['occupant']['badge_valid?'] = badge
  end

  def deny(message)
    @state['request_status'] = 'denied'
    @state['message'] = message
    @state['status'] = nil
  end

  def door_key(door)
    "#{door}_door"
  end

  def status_for
    return @state['status'] if @state['status']

    if @state['pressure'] == 'pressurized' && @state['inner_door']['lock'] == 'unlocked' && @state['outer_door']['lock'] == 'locked'
      'ready for boarding'
    elsif @state['pressure'] == 'depressurized' && @state['inner_door']['lock'] == 'locked' && @state['outer_door']['lock'] == 'unlocked'
      'ready for exit'
    else
      'idle'
    end
  end
end

def load_state(path)
  return SpaceAirlock.new unless File.exist?(path)

  airlock = SpaceAirlock.new
  state = JSON.parse(File.read(path))
  airlock.instance_variable_set(:@state, state)
  airlock
end

def save_state(path, airlock)
  FileUtils.mkdir_p(File.dirname(path))
  File.write(path, JSON.pretty_generate(airlock.instance_variable_get(:@state)))
end

if $PROGRAM_NAME == __FILE__
  state_path = ARGV.shift
  command = ARGV.shift
  airlock = load_state(state_path)

  case command
  when 'reset'
    airlock.reset
    save_state(state_path, airlock)
  when 'set_occupant'
    airlock.send(:set_occupant, ARGV[0], ARGV[1])
    save_state(state_path, airlock)
  when 'set_suit'
    airlock.send(:set_suit, ARGV[0] == 'true')
    save_state(state_path, airlock)
  when 'set_badge'
    airlock.send(:set_badge, ARGV[0] == 'true')
    save_state(state_path, airlock)
  when 'set_door_position'
    airlock.instance_variable_get(:@state)[ARGV[0]]['position'] = ARGV[1]
    save_state(state_path, airlock)
  when 'set_door_lock'
    airlock.instance_variable_get(:@state)[ARGV[0]]['lock'] = ARGV[1]
    save_state(state_path, airlock)
  when 'set_pressure'
    airlock.instance_variable_get(:@state)['pressure'] = ARGV[0]
    save_state(state_path, airlock)
  when 'engage_override'
    airlock.emergency_override_engaged
    save_state(state_path, airlock)
  when 'request_exit'
    airlock.request_exit(ARGV[0])
    save_state(state_path, airlock)
  when 'command_open_door'
    airlock.open_door(ARGV[0])
    save_state(state_path, airlock)
  when 'depressurize'
    airlock.depressurization_commanded
    save_state(state_path, airlock)
  when 'repressurize'
    airlock.repressurization_commanded
    save_state(state_path, airlock)
  when 'get_pressure'
    puts airlock.instance_variable_get(:@state)['pressure']
  when 'get_door_lock'
    puts airlock.instance_variable_get(:@state)[ARGV[0]]['lock']
  when 'get_message'
    puts(airlock.instance_variable_get(:@state)['message'] || '')
  when 'get_status'
    puts airlock.send(:status_for)
  when 'get_denied'
    puts(airlock.instance_variable_get(:@state)['request_status'] == 'denied')
  else
    warn "Unknown command: #{command}"
    exit 1
  end
end
