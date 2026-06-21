import http from '../axios'

export default {
  chat(data) {
    return http({
      url: '/agent/chat',
      method: 'post',
      data
    })
  }
}
