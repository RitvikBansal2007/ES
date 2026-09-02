package ac.iiit.rtltutor.rtl

import ac.iiit.rtltutor.models.ExperimentState
import java.util.Timer
import java.util.TimerTask
import kotlin.math.exp
import kotlin.math.sin

/**
 * RTLSimulator — generates synthetic RC circuit data for offline use.
 * Emits an [ExperimentState] every 1 second simulating a charging RC circuit.
 */
class RTLSimulator {

    private var timer: Timer? = null
    private var time = 0.0
    private var onStateCallback: ((ExperimentState) -> Unit)? = null

    // RC circuit parameters
    private val supplyVoltage = 5.0   // V
    private val resistance   = 1000.0 // Ω
    private val capacitance  = 0.0001 // F (100μF)
    private val tau = resistance * capacitance // time constant

    /**
     * Start emitting simulated [ExperimentState] every 1 second.
     * @param onState callback for each new state
     */
    fun start(onState: (ExperimentState) -> Unit) {
        onStateCallback = onState
        time = 0.0
        timer = Timer().also {
            it.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    val voltage  = supplyVoltage * (1 - exp(-time / tau))
                    val current  = (supplyVoltage / resistance) * exp(-time / tau) * 1000 // mA
                    val frequency = 100.0 + sin(time) * 5.0 // slight wobble

                    val state = ExperimentState(
                        voltage   = voltage,
                        current   = current,
                        frequency = frequency,
                        timestamp = System.currentTimeMillis(),
                        rawJson   = """{"v":$voltage,"i":$current,"f":$frequency}"""
                    )
                    onStateCallback?.invoke(state)
                    time += 1.0
                }
            }, 0L, 1000L)
        }
    }

    /**
     * Stop the simulation and release the timer.
     */
    fun stop() {
        timer?.cancel()
        timer = null
        onStateCallback = null
        time = 0.0
    }
}
