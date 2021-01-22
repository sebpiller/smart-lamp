import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import './vendor';
import { SmartlampamqpwebSharedModule } from 'app/shared/shared.module';
import { SmartlampamqpwebCoreModule } from 'app/core/core.module';
import { SmartlampamqpwebAppRoutingModule } from './app-routing.module';
import { SmartlampamqpwebHomeModule } from './home/home.module';
import { SmartlampamqpwebEntityModule } from './entities/entity.module';
// jhipster-needle-angular-add-module-import JHipster will add new module here
import { MainComponent } from './layouts/main/main.component';
import { NavbarComponent } from './layouts/navbar/navbar.component';
import { FooterComponent } from './layouts/footer/footer.component';
import { PageRibbonComponent } from './layouts/profiles/page-ribbon.component';
import { ErrorComponent } from './layouts/error/error.component';

@NgModule({
  imports: [
    BrowserModule,
    SmartlampamqpwebSharedModule,
    SmartlampamqpwebCoreModule,
    SmartlampamqpwebHomeModule,
    // jhipster-needle-angular-add-module JHipster will add new module here
    SmartlampamqpwebEntityModule,
    SmartlampamqpwebAppRoutingModule,
  ],
  declarations: [MainComponent, NavbarComponent, ErrorComponent, PageRibbonComponent, FooterComponent],
  bootstrap: [MainComponent],
})
export class SmartlampamqpwebAppModule {}
