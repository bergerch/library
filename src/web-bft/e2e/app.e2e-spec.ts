import { WebBftPage } from './app.po';

describe('web-bft App', function() {
  let page: WebBftPage;

  beforeEach(() => {
    page = new WebBftPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
