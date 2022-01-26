import {MessageResponse} from "../sevices/types";
import {ToastService} from "../utils/toast.service";

export class BaseFormComponent {

  constructor(private toastService: ToastService) {
  }

  messages: Array<MessageResponse> = []

  protected handleErrorResponse(error: any) {

    if (typeof error.error === 'object' && 'messages' in error.error) {
      console.log(error)
      this.messages = error.error.messages

      for (let message of error.error.messages) {
        if (message.attribute == null) {
          this.toastService.show(message.code, {classname: 'bg-danger text-light', delay: 15000})
        }
      }
    }
  }

}
