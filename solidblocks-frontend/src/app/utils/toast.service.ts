import {Injectable, TemplateRef} from '@angular/core';
import {MessageResponse} from "../sevices/types";

@Injectable({providedIn: 'root'})
export class ToastService {

  toasts: any[] = [];

  show(textOrTpl: string | TemplateRef<any>, options: any = {}) {
    this.toasts.push({textOrTpl, ...options});
  }

  remove(toast: any) {
    this.toasts = this.toasts.filter(t => t !== toast);
  }

  handleErrorResponse(error: any) {
    if (typeof error.error === 'object' && 'messages' in error.error) {
      this.showMessagesWithoutAttribute(error.error.messages)
    }
  }

  showMessagesWithoutAttribute(messages: Array<MessageResponse>) {
    for (let message of messages) {
      if (message.attribute == null) {
        this.show(message.code, {classname: 'bg-danger text-light', delay: 10000})
      }
    }
  }

}
