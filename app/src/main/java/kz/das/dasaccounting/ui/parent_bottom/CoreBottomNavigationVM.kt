package kz.das.dasaccounting.ui.parent_bottom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kz.das.dasaccounting.core.ui.utils.SingleLiveEvent
import kz.das.dasaccounting.core.ui.view_model.BaseVM
import kz.das.dasaccounting.domain.OfficeInventoryRepository
import kz.das.dasaccounting.domain.ShiftRepository
import kz.das.dasaccounting.domain.data.Location
import kz.das.dasaccounting.domain.data.office.QrSession
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CoreBottomNavigationVM: BaseVM(), KoinComponent {

    private val shiftRepository: ShiftRepository by inject()
    private val officeInventoryRepository: OfficeInventoryRepository by inject()

    private val isControlOptionsShowLV = MutableLiveData<Boolean>()
    fun isControlOptionsShow(): LiveData<Boolean> = isControlOptionsShowLV

    private val isWorkStartedLV = SingleLiveEvent<Boolean>()
    fun isWorkStarted(): LiveData<Boolean> = isWorkStartedLV

    private val isWorkStoppedLV = SingleLiveEvent<Boolean>()
    fun isWorkStopped(): LiveData<Boolean> = isWorkStoppedLV

    private val isRefreshLV = MutableLiveData<Boolean>()
    fun isRefresh(): LiveData<Boolean> = isRefreshLV

    fun setRefresh(refresh: Boolean) = isRefreshLV.postValue(refresh)

    fun setControlOptionsState(isShow: Boolean) = isControlOptionsShowLV.postValue(isShow)

    fun getUserRole() = userRepository.getUserRole()

    fun isOnWork() = userRepository.userOnWork()

    init {
        checkShiftState()
        retrieveNomenclatures()
        retrieveTransferHistory()
    }

    fun saveLocation(long: Double, lat: Double) {
        userRepository.saveLastLocation(Location(long, lat))
    }

    fun getLocation(): Location {
        return userRepository.getLastLocation()
    }

    fun startWork() {
        viewModelScope.launch {
            showLoading()
            try {
                userRepository.startWork()
                shiftRepository.startShift(
                        userRepository.getLastLocation().lat,
                        userRepository.getLastLocation().long,
                        System.currentTimeMillis()
                ).catch {
                    if (it is SocketTimeoutException
                            || it is UnknownHostException
                            || it is ConnectException
                    ) {
                        shiftRepository.saveAwaitStartShift(userRepository.getLastLocation().lat,
                                userRepository.getLastLocation().long,
                                System.currentTimeMillis())
                        setControlOptionsState(isOnWork())
                        isWorkStartedLV.postValue(true)
                    } else {
                        throwableHandler.handle(it)
                    }
                }.collect {
                    shiftRepository.clearAwaitStartWork()

                    setControlOptionsState(isOnWork())
                    isWorkStartedLV.postValue(true)
                }
            } finally {
                hideLoading()
            }
        }
    }

    fun startWork(qrScan: String) {
        viewModelScope.launch {
            showLoading()
            try {
                userRepository.startWork()
                userRepository.saveStartQrScan(qrScan)

                shiftRepository.startShift(
                        userRepository.getLastLocation().lat,
                        userRepository.getLastLocation().long,
                        System.currentTimeMillis(), qrScan
                ).catch {
                    if (it is SocketTimeoutException
                            || it is UnknownHostException
                            || it is ConnectException
                    ) {
                        shiftRepository.saveAwaitStartShift(userRepository.getLastLocation().lat,
                                userRepository.getLastLocation().long,
                                System.currentTimeMillis(), qrScan)
                        setControlOptionsState(isOnWork())
                        isWorkStartedLV.postValue(true)
                    } else {
                        throwableHandler.handle(it)
                    }
                }.collect {
                    shiftRepository.clearAwaitStartWork()

                    setControlOptionsState(isOnWork())
                    isWorkStartedLV.postValue(true)
                }
            } finally {
                hideLoading()
            }
        }
    }

    fun isQrSessionEqual(currentQrScan: String): Boolean {
        val gson = Gson()
        val currentQrSession = gson.fromJson(currentQrScan, QrSession::class.java)
        val startQrSession = gson.fromJson(userRepository.getStartQrScan(), QrSession::class.java)

        return currentQrSession.uuid == startQrSession.uuid
    }

    fun stopWork() {
        viewModelScope.launch {
            try {
                shiftRepository.finishShift(
                        userRepository.getLastLocation().lat,
                        userRepository.getLastLocation().long,
                        System.currentTimeMillis()
                ).catch {
                    if (it is SocketTimeoutException
                            || it is UnknownHostException
                            || it is ConnectException
                    ) {
                        shiftRepository.saveAwaitFinishShift(userRepository.getLastLocation().lat,
                                userRepository.getLastLocation().long,
                                System.currentTimeMillis())
                        setControlOptionsState(isOnWork())
                        isWorkStoppedLV.postValue(true)
                    } else {
                        throwableHandler.handle(it)
                    }
                }.collect {
                    userRepository.stopWork()
                    shiftRepository.clearAwaitFinishWork()

                    setControlOptionsState(isOnWork())
                    isWorkStoppedLV.postValue(true)
                }
            } finally {
                hideLoading()
            }
        }
    }

    private fun retrieveNomenclatures() {
        viewModelScope.launch {
            try {
                officeInventoryRepository.getNomenclatures()
            } catch (t: Throwable) {
                throwableHandler.handle(t)
            }
        }
    }

    private fun retrieveTransferHistory() {
        viewModelScope.launch {
            try {
                userRepository.getHistoryOfficeInventories()
                userRepository.getHistoryTransportInventories()
                userRepository.getHistoryWarehouseInventories()
            } catch (t: Throwable) {
                throwableHandler.handle(t)
            }
        }
    }

    private fun checkShiftState() {
        viewModelScope.launch {
            try {
                val state = shiftRepository.isShiftState().opened
                if (state) {
                    isWorkStartedLV.postValue(true)
                    userRepository.startWork()
                } else {
                    isWorkStoppedLV.postValue(true)
                    userRepository.stopWork()
                }
                setControlOptionsState(state)
            } catch (t: Throwable) {

            }
        }
    }

}