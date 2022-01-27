import {MessageResponse} from "../sevices/types";
import {ToastService} from "../utils/toast.service";

export class BaseFormComponent {

  messages: Array<MessageResponse> = []

  constructor(private toastService: ToastService) {
  }

  protected handleErrorResponse(error: any) {
    if (typeof error.error === 'object' && 'messages' in error.error) {
      this.messages = error.error.messages
      this.toastService.showMessagesWithoutAttribute(error.error.messages)
    }
  }
}
