import {Injectable} from '@angular/core';
import {AuthenticationClient} from './AuthenticationClient';
import {IUserCreationRequest} from 'abcmap-shared';
import {ToastService} from '../notifications/toast.service';
import {tap} from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthenticationService {

  constructor(private client: AuthenticationClient,
              private toasts: ToastService) {
  }

  public registerUser(request: IUserCreationRequest) {
    return this.client.registerUser(request)
      .pipe(
        tap(res => this.toasts.info('Vous êtes inscrit !'),
          err => this.toasts.error('Erreur lors de l\'inscription, veuillez réessayer plus tard !'))
      );
  }
}
