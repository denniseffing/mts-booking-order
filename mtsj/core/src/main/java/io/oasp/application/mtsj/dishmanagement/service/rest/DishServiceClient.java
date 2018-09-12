package io.oasp.application.mtsj.dishmanagement.service.rest;

import io.oasp.application.mtsj.dishmanagement.logic.api.to.DishEto;
import io.oasp.application.mtsj.dishmanagement.logic.api.to.IngredientEto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

@FeignClient(name = "service-dishes", url = "http://mts-dishes")
@Qualifier("dishServiceClient")
public interface DishServiceClient {

    @RequestMapping(value = "/v1/dish/{id}", method = RequestMethod.GET)
    DishEto getDish(@PathVariable("id") Long dishId);

    @RequestMapping(value = "/v1/ingredient/{ids}", method = RequestMethod.GET)
    List<IngredientEto> getIngredients(@PathVariable("ids") List<Long> ingredientId);
}
